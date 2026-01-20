import { useState, useEffect, useCallback } from 'react';
import { fetchCollectionMetadata } from '../api/chat';

const DEFAULT_LOGO = 'https://docs.dataminesoftware.com/Assets/Images/Datamine-Logo.png';

function getCollectionFromUrl() {
    const hash = window.location.hash.slice(1);
    return hash || null;
}

export function useCollection() {
    const [urlCollection, setUrlCollection] = useState(getCollectionFromUrl);
    const [resolvedCollection, setResolvedCollection] = useState(getCollectionFromUrl);
    const [isLoading, setIsLoading] = useState(true);
    const [title, setTitle] = useState('Product Documentation');
    const [logo, setLogo] = useState(DEFAULT_LOGO);

    const loadMetadata = useCallback(async (collectionName) => {
        setIsLoading(true);
        const metadata = await fetchCollectionMetadata(collectionName);
        console.log('Collection metadata loaded:', { collectionName, metadata });
        setTitle(metadata.title);
        setLogo(metadata.logo);
        setResolvedCollection(metadata.resolvedCollection);
        setIsLoading(false);

        // Update document title
        if (collectionName) {
            document.title = `Datamine Help - ${collectionName}`;
        } else {
            document.title = 'Datamine Help';
        }
    }, []);

    useEffect(() => {
        // Reset resolved collection to URL collection immediately, then load actual resolved value
        setResolvedCollection(urlCollection);
        loadMetadata(urlCollection);
    }, [urlCollection, loadMetadata]);

    useEffect(() => {
        const handleHashChange = () => {
            const newCollection = getCollectionFromUrl();
            setUrlCollection(newCollection);
            setResolvedCollection(newCollection); // Reset immediately on hash change
        };

        window.addEventListener('hashchange', handleHashChange);
        return () => window.removeEventListener('hashchange', handleHashChange);
    }, []);

    return {
        urlCollection,
        resolvedCollection,
        isLoading,
        title,
        logo
    };
}
